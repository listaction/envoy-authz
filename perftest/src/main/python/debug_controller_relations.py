
from locust import HttpUser, task

# Run as:
# locust --headless --users 3 --spawn-rate 1 --host http://localhost:8183  -f .\debug_controller_relations.py
class AuthTest(HttpUser):
    service_url = ""
    auth = ""

    def init(self, auth=""):
        self.auth = auth

    @task
    def testTask(self):
        self.test("test", "coarse-access", "aea87095-7278-4655-8184-0c746a060614")

    def test(self, namespace, object, principal):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        params = {"namespace": namespace, "object": object, "principal": principal}
        return self.client.get("/debug/relations", headers=headers, params=params)
