// Validate token

async function validateToken() {
  const errorDiv = document.getElementById('error');

  errorDiv.textContent = '';  // Clear previous errors

  try {
    model = {Token: document.getElementById('totptoken').value};
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
      if (result.Valid === true) {
        window.location.href =
            '/home.html?username=' + localStorage.getItem('username');
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
