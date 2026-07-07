#!/bin/sh
# cert_exchange.sh
# Copies /etc/ssl/certs/self_ca.pem to the destination BMC and creates the
# OpenSSL subject-name hash symlink in /etc/ssl/certs/authority/.
#
# Usage: cert_exchange.sh <dest_ip> <dest_port> <dest_user> <dest_password>

set -e

DEST_IP="$1"
DEST_PORT="$2"
DEST_USER="$3"
DEST_PASS="$4"

SRC_CERT="/etc/ssl/certs/self_ca.pem"
DEST_CERT="/etc/ssl/certs/authority/ca.pem"
DEST_DIR="/etc/ssl/certs/authority"

if [ -z "$DEST_IP" ] || [ -z "$DEST_PORT" ] || [ -z "$DEST_USER" ] || [ -z "$DEST_PASS" ]; then
    echo "Usage: $0 <dest_ip> <dest_port> <dest_user> <dest_password>"
    exit 1
fi

if [ ! -f "$SRC_CERT" ]; then
    echo "ERROR: Source certificate not found: $SRC_CERT"
    exit 1
fi

echo "==> Copying $SRC_CERT to $DEST_USER@$DEST_IP:$DEST_CERT ..."

# Use sshpass + scp to perform the transfer non-interactively
sshpass -p "$DEST_PASS" scp \
    -P "$DEST_PORT" \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    "$SRC_CERT" \
    "$DEST_USER@$DEST_IP:$DEST_CERT"

echo "==> Certificate copied successfully."

# Compute the OpenSSL subject-name hash for the certificate we just copied
HASH=$(openssl x509 -noout -subject_hash -in "$SRC_CERT")
if [ -z "$HASH" ]; then
    echo "ERROR: Failed to compute certificate hash"
    exit 1
fi
echo "==> Certificate hash: $HASH"

# Create the hash symlink on the destination via SSH
echo "==> Creating hash symlink on destination ..."
sshpass -p "$DEST_PASS" ssh \
    -p "$DEST_PORT" \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    "$DEST_USER@$DEST_IP" \
    "cd $DEST_DIR && \
     IDX=0 && \
     while [ -e \"${HASH}.\${IDX}\" ] && [ \$IDX -lt 100 ]; do IDX=\$((IDX+1)); done && \
     if [ \$IDX -ge 100 ]; then echo 'ERROR: too many hash collisions'; exit 1; fi && \
     ln -sf ca.pem \"${HASH}.\${IDX}\" && \
     echo \"Created symlink: ${HASH}.\${IDX} -> ca.pem\""

echo "==> Certificate exchange complete."
