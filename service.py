from flask import Flask, jsonify

app = Flask(__name__)
app.config['JSONIFY_PRETTYPRINT_REGULAR'] = False

@app.route('/service/<service_number>')
def rules(service_number):
    results = [
        {
           "resource": "/res1",
           "allowed": True
        },
        {
            "resource": "/res2",
            "allowed": False
        }
    ]
    return jsonify(results)



if __name__ == "__main__":
  app.run(host='0.0.0.0', port=8002, debug=True)
