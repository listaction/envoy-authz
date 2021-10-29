
import sys
import uuid

from locust import HttpUser, task

# Run as:
# locust --headless --users 3 --spawn-rate 1 --host http://localhost:8183  -f .\debug_contoller_test.py
class AuthTest(HttpUser):
    service_url = ""
    auth = ""

    def init(self, auth=""):
        self.auth = auth

    @task
    def testTask(self):

        test_resp = self.test()
        if test_resp.status_code == 200:
            print("[OK] Data: " + test_resp.text)
        else:
            print("FAILED status: " + str(test_resp.status_code) + ", text: " + test_resp.text)
            sys.exit(1)

    def test(self):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        json = {"namespace": "test", "object": "TB", "relation": "coarse-access", "principal": str(uuid.uuid4())}
        return self.client.post("/acl/create", headers=headers, json=json)
