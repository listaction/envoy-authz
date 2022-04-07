
import sys
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
        # test_resp = self.test("test", "coarse-access", "TB", "0003d627-3a09-4891-84ff-8c624dcefa76")
        # test_resp = self.test("test", "groups", "TB", "0003d627-3a09-4891-84ff-8c624dcefa76")
        #
        test_resp = self.test("test", "coarse-access", "TB", "89a13a86-d5c2-4701-9ac7-2d8402e1f1d3")
        # test_resp = self.test("test", "groups", "TB", "89a13a86-d5c2-4701-9ac7-2d8402e1f1d3")
        if test_resp.status_code == 200:
            print("[OK] Data: " + test_resp.text)
        else:
            print("FAILED status: " + str(test_resp.status_code) + ", text: " + test_resp.text)
            sys.exit(1)

    def test(self, namespace, object, relation, principal):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        params = {"namespace": namespace, "object": object, "relation": relation, "principal": principal}
        return self.client.get("/debug/test", headers=headers, params=params)
