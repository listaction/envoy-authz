import requests
import sys
import urllib3


class AuthTest:
    service_url = ""
    auth = ""

    def init(self, service_url="http://localhost:18000", auth=""):
        self.service_url = service_url
        self.auth = auth

    def create(self):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        response = requests.post(self.service_url + "/", headers=headers, verify=False)
        return response

    def update(self, contact_id):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        response = requests.put(self.service_url + "/" + contact_id, headers=headers, verify=False)
        return response

    def get(self, contact_id):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        response = requests.get(self.service_url + "/" + contact_id, headers=headers, verify=False)
        return response

    def delete(self, contact_id):
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + self.auth
        }

        response = requests.delete(self.service_url + "/" + contact_id, headers=headers, verify=False)
        return response

def main(args):
    urllib3.disable_warnings()

    api1 = AuthTest()
    api1.init(auth="user1", service_url="http://localhost:18000/contact")
    print("user1 creating new contact")
    create = api1.create()
    if create.status_code == 200:
        contact_id = create.text
        print("[OK] ContactId: " + contact_id)
    else:
        print("FAILED")
        sys.exit(1)

    print("user1 getting the contact")
    get_resp1 = api1.get(contact_id=contact_id)
    if get_resp1.status_code == 200:
        print("[OK] Data: " + get_resp1.text)
    else:
        print("FAILED")
        sys.exit(1)

    print("user1 updating the contact")
    upd_resp1 = api1.update(contact_id=contact_id)
    if upd_resp1.status_code == 200:
        print("[OK] Updated")
    else:
        print("FAILED")
        sys.exit(1)


    api2 = AuthTest()
    api2.init(auth="user2", service_url="http://localhost:18000/contact")
    print("user2 reading the contact")
    get_resp2 = api2.get(contact_id=contact_id)
    if get_resp2.status_code == 200:
        print("[OK] Data: " + get_resp2.text)
    else:
        print("FAILED")
        sys.exit(1)

    print("user2 updating the contact")
    upd_resp2 = api2.update(contact_id=contact_id)
    if upd_resp2.status_code == 200:
        print("[OK] Updated")
    else:
        print("FAILED")
        sys.exit(1)

    api3 = AuthTest()
    api3.init(auth="user3", service_url="http://localhost:18000/contact")
    print("user3 deleting the contact")
    del_resp2 = api3.delete(contact_id=contact_id)
    if del_resp2.status_code == 403:
        print("[OK] Unable to delete. User has only update permission. Owner permission is required to delete item.")
    else:
        print("FAILED")
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv)

