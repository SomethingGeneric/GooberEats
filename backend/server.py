from flask import Flask, request

import os

app = Flask(__name__)

if not os.path.exists('cal_data'):
    os.makedirs('cal_data')

def get_glob_kcount(id):
    try:
        with open(f'cal_data/{id}.txt', 'r') as file:
            return int(file.read())
    except FileNotFoundError:
        with open(f'cal_data/{id}.txt', 'w') as file:
            file.write('0')
        return 0

def set_glob_kcount(id, value):
    with open(f'cal_data/{id}.txt', 'w') as file:
        file.write(str(value))

@app.route('/kcount', methods=['GET', 'POST'])
def deal():
    if request.method == 'GET':
        id = request.args.get('id')
        return str(get_glob_kcount(id))
    elif request.method == 'POST':
        data = request.get_json()
        if 'id' in data and 'kcal' in data and isinstance(data['kcal'], int):
            id = data['id']
            increment = data['kcal']
            glob_kcount = get_glob_kcount(id)
            glob_kcount += increment
            set_glob_kcount(id, glob_kcount)
            return str(glob_kcount)
        else:
            return "Invalid request body!"

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)
