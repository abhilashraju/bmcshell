async function createSecretKey(callback) {
  try {
    // Construct the API URL using the current domain
    const apiUrl = `/redfish/v1/AccountService/Accounts/${
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
          data.GenerateSecretKeyResponse.SecretKeyUrl, callback);


    } else {
      const data = await response.text();
      console.error('Secret key creation failed:', data);
    }
  } catch (error) {
    console.error('Error during secret key creation:', error);
  }
}