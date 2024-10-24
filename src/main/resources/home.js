// Function to get query parameters
let model = {};
function getQueryParams() {
  const params = {};
  const queryString = window.location.search.substring(1);
  const regex = /([^&=]+)=([^&]*)/g;
  let m;
  while (m = regex.exec(queryString)) {
    params[decodeURIComponent(m[1])] = decodeURIComponent(m[2]);
  }
  return params;
}

// Get the username from the query parameters
const params = getQueryParams();
const username = params['username'] || 'Guest';

// Function to submit the selected bypass option
async function submitBypassOption() {
  const selectedOption = document.getElementById('bypass-options').value;
  uri =
      `/redfish/v1/AccountService/Accounts/${localStorage.getItem('username')}`;
  data = {
    MFABypass: {
      BypassTypes:
          'xyz.openbmc_project.User.MFABypass.MFABypassType.' + selectedOption
    }
  };
  try {
    const response = await fetch(uri, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Token': localStorage.getItem('xAuthToken')
      },
      body: JSON.stringify(data)
    });

    if (response.ok) {
      const responseData = await response.text();
      console.log('Request successful:', responseData);
      showErrorMessage('blue', 'Bypass option updated successfully.');
    } else {
      showErrorMessage(
          'red', 'An error occurred while updating bypass option.');
      console.error('Request failed:', response.status, response.statusText);
    }
  } catch (error) {
    showErrorMessage('red', 'An error occurred while updating bypass option.');
    console.error('Request error:', error);
  }
}
function showErrorMessage(color, message) {
  const errorMessage = document.getElementById('error-message');
  errorMessage.style.color = color;
  errorMessage.textContent = message;
  errorMessage.style.display = 'block';
  setTimeout(() => {
    errorMessage.style.display = 'none';
    location.reload();
  }, 5000);  // Hide after 5 seconds
}
document.addEventListener('DOMContentLoaded', function() {
  let googleCheckbox = document.getElementById('google-authenticator');
  let microsoftCheckbox = document.getElementById('microsoft-authenticator');
  const errorMessage = document.getElementById('error-message');
  fetch('/redfish/v1/AccountService/', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'X-Auth-Token': localStorage.getItem('xAuthToken')
    }
  })
      .then(response => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.json();
      })
      .then(data => {
        console.log('Account Service Data:', data);
        const googleAuthenticatorCheckbox =
            document.getElementById('google-authenticator');
        googleAuthenticatorCheckbox.checked =
            data.MultiFactorAuth.GoogleAuthenticator.Enabled;

        // Handle the data as needed
      })
      .catch(error => {
        console.error('There was a problem with the fetch operation:', error);
      });
  fetch('/redfish/v1/AccountService/Accounts/' + username, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'X-Auth-Token': localStorage.getItem('xAuthToken')
    }
  })
      .then(response => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.json();
      })
      .then(data => {
        console.log('Account Data:', data);
        const bypassOptions =
            data.MFABypass;  // Assuming the response is { "bypassOptions":
                             // ["GoogleAuthenticator", "None"] }

        const checkbox = document.querySelector(
            `input[name="bypass-option"][value="${bypassOptions}"]`);
        if (checkbox) {
          checkbox.checked = true;
        }

        // Handle the data as needed
      })
      .catch(error => {
        console.error('There was a problem with the fetch operation:', error);
      });


  document.getElementById('submit-button')
      .addEventListener('click', function() {
        const checkboxes =
            document.querySelectorAll('input[name="mfa-option"]:checked');
        const data = {MultiFactorAuth: {GoogleAuthenticator: {Enabled: false}}};
        checkboxes.forEach((checkbox) => {
          data.MultiFactorAuth[checkbox.value] = {Enabled: true};
        });
        console.log(JSON.stringify(data));  // For demonstration purposes

        // You can send the data using fetch or any other method
        // Example using fetch:
        fetch('/redfish/v1/AccountService/', {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
            'X-Auth-Token': localStorage.getItem('xAuthToken')
          },
          body: JSON.stringify(data)
        })
            .then(response => {
              if (!response.ok) {
                throw new Error('Network response was not ok');
              }
              return console.log(response);
            })
            .catch((error) => {
              console.error('Error:', error);
            });
      });
});

document.getElementById('submit-bypass-button')
    .addEventListener('click', function() {
      const checkboxes =
          document.querySelectorAll('input[name="bypass-option"]:checked');

      let bypassTypes = [];
      bypassTypes.push(checkboxes.length > 0 ? checkboxes[0].value : 'None');
      const data = {MFABypass: {BypassTypes: bypassTypes}};

      console.log(JSON.stringify(data));  // For demonstration purposes
      uri = `/redfish/v1/AccountService/Accounts/${
          localStorage.getItem('username')}`;
      // Send the data using fetch
      fetch(uri, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'X-Auth-Token': localStorage.getItem('xAuthToken')
        },
        body: JSON.stringify(data)
      })
          .then(response => {
            if (!response.ok) {
              throw new Error('Network response was not ok');
            }
            return console.log(response);
          })
          .catch((error) => {
            console.error('Error:', error);
          });
    });
function resetPopupContent() {
  const popupContent = document.getElementById('popup-content');
  if (popupContent) {
    popupContent.innerHTML = '';
  }
}