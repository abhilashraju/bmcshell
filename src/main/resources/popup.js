// Validate token


async function validateToken() {
  return validateTokenWithCallback((result) => {
    popupSuccessCallback(result);
    // if (result) {
    //   return window.location.href =
    //              '/home.html?username=' + localStorage.getItem('username');
    // }
  });
}
async function validateTokenWithCallback(callback) {
  const errorDiv = document.getElementById('error');

  errorDiv.textContent = '';  // Clear previous errors

  try {
    model = {TimeBasedOneTimePassword: document.getElementById('totptoken').value};
    uri = `/redfish/v1/AccountService/Accounts/${
        localStorage.getItem(
            'username')}/Actions/ManagerAccount.VerifyTimeBasedOneTimePassword`;
    const response = await fetch(uri, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Token': localStorage.getItem('xAuthToken')
      },
      body: JSON.stringify(model)
    });
    if (response.ok) {
      console.log('Success:', response);
      const result = await response.json();
      console.log('Success Result:', result);
      if (response.status === 200) {
        console.log('Token validation successful');
        callback(true);

      } else {
        errorDiv.textContent = 'Validation failed. Please try again.';
      }
    } else {
      console.error('Error:', response);
      const result = await response.text();
      throw new Error(result);
    }


  } catch (error) {
    errorDiv.textContent = 'An error occurred. Please try again.';
  }
}
