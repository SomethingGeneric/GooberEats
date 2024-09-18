import requests

# Make a POST request with JSON data
url = "http://127.0.0.1:5000/kcount"
uid = "dBwuayheC06sO6IDnPfepDKJjHdPi4atm6oiVpOZFu44muXiVNnjLTaAcGI3UKmz"
data = {"id": uid, "kcal": 10}
response = requests.post(url, json=data)

# Check the response status code
if response.status_code == 200:
    print("POST request successful")

# Make a GET request to verify the returned value
response = requests.get(url, params={"id": uid})

# Check the response status code and value
if response.status_code == 200:
    print(response.json())