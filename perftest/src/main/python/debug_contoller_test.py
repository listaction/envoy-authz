
import sys
from locust import HttpUser, task

# Run as:
# locust --headless --users 3 --spawn-rate 1 --host http://localhost:8183/debug  -f .\debug_contoller_test.py
class AuthTest(HttpUser):
    service_url = ""
    auth = ""

    def init(self, auth=""):
        self.auth = auth

    @task
    def testTask(self):
        test_resp = self.test("relation", "object", "relation", "principal")
        # if test_resp.status_code == 200:
        #     print("[OK] Data: " + test_resp.text)
        # else:
        #     print("FAILED status: " + str(test_resp.status_code) + ", text: " + test_resp.text)
        #     sys.exit(1)

    def test(self, namespace, object, relation, principal):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        return self.client.get(f"/test?namespace={namespace}&object={object}&relation={relation}&principal={principal}", headers=headers)
