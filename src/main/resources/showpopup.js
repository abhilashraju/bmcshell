let popupSuccessCallback = null;
function showPopup(secretKey, secretKeyUrl, callback) {
  popupSuccessCallback = callback;
  fetch('popup.html')
      .then(response => response.text())
      .then(htmlData => {
        // Fetch the styles
        fetch('popup.css')
            .then(response => response.text())
            .then(styleData => {
              // Create a style element and append the fetched styles
              const styleElement = document.createElement('style');
              styleElement.textContent = styleData;
              document.head.appendChild(styleElement);

              // Fetch and append the QR code script
              fetch('https://cdn.jsdelivr.net/npm/qrcodejs/qrcode.min.js')
                  .then(response => response.text())
                  .then(qrCodeData => {
                    const scriptElement1 = document.createElement('script');
                    scriptElement1.textContent = qrCodeData;
                    document.head.appendChild(scriptElement1);

                    // Fetch and append the popup.js script
                    fetch('popup.js')
                        .then(response => response.text())
                        .then(scriptData => {
                          const scriptElement2 =
                              document.createElement('script');
                          scriptElement2.textContent = scriptData;
                          document.head.appendChild(scriptElement2);

                          // Parse and insert the HTML content
                          const parser = new DOMParser();
                          const doc =
                              parser.parseFromString(htmlData, 'text/html');
                          const popupContainer = document.createElement('div');
                          popupContainer.id = 'popupContainer';
                          popupContainer.append(...doc.body.children);
                          // Append the popupContainer to the body
                          document.body.appendChild(popupContainer);

                          document.getElementById('username-heading')
                              .innerText =
                              'Validate Token for ' + model.UserName;

                          document.getElementById('secret').innerText =
                              secretKey;

                          // Generate QR code after scripts are loaded
                          const qrCodeContainer =
                              document.getElementById('qrCodeContainer');
                          new QRCode(qrCodeContainer, {
                            text: secretKeyUrl,  // Replace with your
                                                 // QR code data
                            width: 128,
                            height: 128
                          });

                          const closeButton = document.getElementById('close');

                          closeButton.onclick = () => {
                            document.body.removeChild(popupContainer);
                            document.head.removeChild(styleElement);
                            document.head.removeChild(scriptElement1);
                            document.head.removeChild(scriptElement2);
                            callback(false);
                          };
                          // Adjust popup size based on content
                          const popupContent =
                              document.querySelector('.popup-content');
                          popupContent.style.width = 'auto';
                          popupContent.style.height = 'auto';
                          document.getElementById('popup').style.display =
                              'flex';
                        })
                        .catch(
                            error => console.error(
                                'Error fetching popup.js script:', error));
                  })
                  .catch(
                      error => console.error(
                          'Error fetching QR code script:', error));
            })
            .catch(error => console.error('Error fetching styles:', error));
      })
      .catch(error => console.error('Error fetching popup.html:', error));
}

function closePopup() {
  document.getElementById('popup').style.display = 'none';
}
