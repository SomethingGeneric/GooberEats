import requests
from random import randint

# Make a POST request with JSON data
url = "http://127.0.0.1:5000"
uid = "dBwuayheC06sO6IDnPfepDKJjHdPi4atm6oiVpOZFu44muXiVNnjLTaAcGI3UKmz"
data = {"id": uid, "kcal": randint(1, 1000)}
response = requests.post(f"{url}/kcount", json=data)

# Check the response status code
if response.status_code == 200:
    print("POST request successful")

# Make a GET request to verify the returned value
response = requests.get(f"{url}/kcount", params={"id": uid})

# Check the response status code and value
if response.status_code == 200:
    print(response.json())


# Make a GET request to retrieve all data for a user
response = requests.get(f"{url}/datafor", params={"id": uid})
if response.status_code == 200:
    data = response.json()
    for item in data:
        print(item["datestamp"], item["calorie_count"], item["description"])
