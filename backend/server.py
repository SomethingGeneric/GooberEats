from flask import Flask, request

app = Flask(__name__)

def get_glob_kcount():
    try:
        with open('calories.txt', 'r') as file:
            return int(file.read())
    except FileNotFoundError:
        with open('calories.txt', 'w') as file:
            file.write('0')
        return 0

def set_glob_kcount(value):
    with open('calories.txt', 'w') as file:
        file.write(str(value))

@app.route('/kcount', methods=['GET', 'POST'])
def deal():
    if request.method == 'GET':
        return str(get_glob_kcount())
    elif request.method == 'POST':
        data = request.get_json()
        if 'kcal' in data and isinstance(data['kcal'], int):
            increment = data['kcal']
            glob_kcount = get_glob_kcount()
            glob_kcount += increment
            set_glob_kcount(glob_kcount)
            return str(glob_kcount)
        else:
            return "Invalid request body!"

if __name__ == '__main__':
    app.run(debug=True)
