let xAuthToken = null;
let model = {};
const currentDomain = window.location.origin;
function showPopup(secretKey, secretKeyUrl) {
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

document.getElementById('login-form')
    .addEventListener('submit', function(event) {
      event.preventDefault();  // Prevent the default form submission

      const apiUrl = `${currentDomain}/redfish/v1/SessionService/Sessions`;
      console.log('API URL:', apiUrl);

      // Create a model object with form data
      model = {
        UserName: document.getElementById('username').value,
        Password: document.getElementById('password').value,
        Token: document.getElementById('totp').value
      };

      // Make a POST request to the endpoint
      fetch(apiUrl, {
        // Replace with your endpoint URL
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'  // Optional: specify the expected
                                        // response format
        },
        body: JSON.stringify(model)
      })
          .then(response => {
            if (response.ok) {
              console.log('Response Headers:', response.headers);
              // Extract X-Auth-Token from the response header
              xAuthToken = response.headers.get('X-Auth-Token');
              localStorage.setItem('xAuthToken', xAuthToken);
              localStorage.setItem('username', model.UserName);
              console.log('X-Auth-Token:', xAuthToken);
              return response.json();
            }
            return response.text().then(text => {
              throw new Error(text);
            });
          })
          .then(data => {
            console.log('Success:', data);
            if (data.SecretkeyRequired === 'True') {
              return createSecretKey();
            } else {
              return showHome();
            }
          })
          .catch((error) => {
            console.error('Error:', error);
            const errorMessageElement =
                document.getElementById('error-message');
            errorMessageElement.textContent =
                'Invalid credentials. Please try again.';
            errorMessageElement.style.display = 'block';
          });
    });
// Assume username is stored in a variable called `username`
const username =
    'exampleUser';  // Replace with actual username extraction logic

async function showHome() {
  // Construct the URL with the username as a query parameter
  window.location.href =
      '/home.html?username=' + localStorage.getItem('username');
};
async function createSecretKey() {
  try {
    // Construct the API URL using the current domain
    const apiUrl = `${currentDomain}/redfish/v1/AccountService/Accounts/${
        model.UserName}/Actions/ManagerAccount.GenerateSecretKey`;

    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Token': localStorage.getItem('xAuthToken')
      },

    });

    if (response.ok) {
      const data = await response.json();
      console.log('Response Headers:', response.headers);

      console.log('Response Body:', data.GenerateSecretKeyResponse);

      showPopup(
          data.GenerateSecretKeyResponse.SecretKey,
          data.GenerateSecretKeyResponse.SecretKeyUrl);


    } else {
      const data = await response.text();
      console.error('Secret key creation failed:', data);
    }
  } catch (error) {
    console.error('Error during secret key creation:', error);
  }
}
