from flask import Flask, request

import os

from data import caldata

app = Flask(__name__)
cd = caldata()

@app.route('/kcount', methods=['GET', 'POST'])
def deal():
    if request.method == 'GET':
        id = request.args.get('id')
        value = cd.get_current_glob_kcount(id)
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
            cd.add_current_glob_kcount(id, newcal, desc)
            return "Success"
        else:
            return "Invalid request body!"


if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)
