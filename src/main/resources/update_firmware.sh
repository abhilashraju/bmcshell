#!/bin/bash
label=$1
imgpath=$2
version=$3

diskPartition=$(basename "$(readlink -f /dev/disk/by-partlabel/boot-"${label}")")
diskdev=$(basename "$(dirname "$(realpath "/sys/class/block/${diskPartition}")")")
zstd -d -c "${imgpath}"/"${version}"/image-kernel | dd of="/dev/disk/by-partlabel/boot-${label}"
number="$(readlink -f /dev/disk/by-partlabel/boot-"${label}" | grep -o '[0-9]\+$')"
sgdisk --change-name="${number}":boot-"${label}" "/dev/${diskdev}" 1>/dev/null
zstd -d -c "${imgpath}"/"${version}"/image-rofs | dd of="/dev/disk/by-partlabel/rofs-${label}"
number="$(readlink -f /dev/disk/by-partlabel/rofs-"${label}" | grep -o '[0-9]\+$')"
sgdisk --change-name="${number}":rofs-"${label}" "/dev/${diskdev}" 1>/dev/null
partprobe

# Made with Bob
