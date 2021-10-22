
from locust import HttpUser, task

# Run as:
# locust --headless --users 3 --spawn-rate 1 --host http://localhost:8183/debug  -f .\debug_contoller_relations.py
class AuthTest(HttpUser):
    service_url = ""
    auth = ""

    def init(self, auth=""):
        self.auth = auth

    @task
    def testTask(self):
        self.test("test", "coarse-access", "b9403f3b-44ab-47d0-baba-20f813a3e204")

    def test(self, namespace, object, principal):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        params = {"namespace": namespace, "object": object, "principal": principal}
        return self.client.get("/relations", headers=headers, params=params)
