# stdlib
import os

# pip
from flask import Flask, request, render_template
import toml

# local
from data import caldata
from research import Research

config = toml.load('config.toml')

app = Flask(__name__)
cd = caldata()
research = Research(config['openai']['api_key'], config['anthropic']['api_key'])

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
    return render_template('page.html', content="<h1>GooberEats</h1><h2>Actual website coming soon!</h2>")


@app.route('/research')
def research():
    return render_template('page.html', content=render_template('research.html'))

@app.route('/research_results')
def research_results():
    prompt = request.args.get('prompt')
    gpt_response, claude_response = research.combo_query(prompt)
    return render_template('page.html', content=f"<h4>GPT:</h4><pre>{gpt_response}<pre><br/><hr><br/><h4>Claude:</h4><pre>{claude_response}</pre>")

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)
