from flask import Flask, request

import os

from data import caldata

app = Flask(__name__)
cd = caldata()

@app.route('/api/kcount', methods=['GET', 'POST'])
def deal():
    """
    Handle GET and POST requests for '/api/kcount' endpoint.

    For GET requests, retrieves the current kcount value for a given 'id' parameter.
    For POST requests, adds calories to the current kcount for a given 'id' parameter.

    Returns:
        - For GET requests: The current global kcount value as a string. (clients deal with typecasting as needed)
        - For POST requests: "Success" if the calories were added successfully, or "Invalid request body!" if the request body is invalid.
    """
    if request.method == 'GET':
        id = request.args.get('id')
        value = cd.get_current_kcount(id)
        print(f"GET request for id={id}: {value}")
        return str(value)
    elif request.method == 'POST':
        data = request.get_json()
        if 'id' in data and 'kcal' in data and isinstance(data['kcal'], int):
            id = data['id']
            newcal = data['kcal']
            desc = "Not logged"
            if 'desc' in data:
                desc = data['desc']
            print(f"POST request - Adding calories id={id} by {newcal}")
            cd.add_current_kcount(id, newcal, desc)
            return "Success"
        else:
            return "Invalid request body!"

@app.route("/api/datafor", methods=['GET'])
def get_data():
    """
    Get all entries for a given id.

    Returns:
        str: The result of calling `cd.get_all_kcal(id)`, which is JSON.
    """
    id = request.args.get('id')
    print(f"GET request for all data of id={id}")
    return str(cd.get_all_kcal(id))

# END OF API ENDPOINTS

@app.route('/')
def home():
    return "GooberEats<br/>Actual website coming soon!"

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)
