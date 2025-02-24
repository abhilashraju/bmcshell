let xAuthToken = null;
let model = {};
const currentDomain = window.location.origin;

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
            if (response.ok || response.status === 403) {
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
          var error = data['error'];
          if (error && error['@Message.ExtendedInfo'] && Array.isArray(error['@Message.ExtendedInfo']) &&
          error['@Message.ExtendedInfo'].length > 0 &&
          error['@Message.ExtendedInfo'][0].MessageId &&
          error['@Message.ExtendedInfo'][0].MessageId.includes('GenerateSecretKeyRequired')) {
              return createSecretKey((response) => {
                if (response) {
                  return showHome();
                }
                return showErrorMessage(
                    'red', 'Secret key confirmation required');
              });
            } else {
              return showHome();
            }
          })
          .catch((error) => {
            console.error('Error:', error);
            showErrorMessage('red', error.message);
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
function showErrorMessage(color, message) {
  const errorMessage = document.getElementById('error-message');
  errorMessage.style.color = color;
  errorMessage.textContent = message;
  errorMessage.style.display = 'block';
  setTimeout(() => {
    errorMessage.style.display = 'none';
  }, 5000);  // Hide after 5 seconds
}