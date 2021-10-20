import requests
import sys
import urllib3

class AuthTest:
    service_url = ""
    auth = ""

    def init(self, service_url="http://localhost:18000", auth=""):
        self.service_url = service_url
        self.auth = auth

    def test(self, namespace, object, relation, principal):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        params = {"namespace": namespace, "object": object, "relation": relation, "principal": principal}
        response = requests.get(self.service_url + "/test", headers=headers, verify=False, params=params)
        return response

def main(args):
    urllib3.disable_warnings()

    t = AuthTest()
    t.init(auth="user1", service_url="http://localhost:8183/debug")

    test_resp = t.test("relation", "object", "relation", "principal")
    if test_resp.status_code == 200:
        print("[OK] Data: " + test_resp.text)
    else:
        print("FAILED status: " + str(test_resp.status_code) + ", text: " + test_resp.text)
        sys.exit(1)

if __name__ == "__main__":
    main(sys.argv)