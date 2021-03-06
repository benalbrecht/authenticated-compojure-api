(ns authenticated-compojure-api.permission.permission-creation-tests
  (:require [clojure.test :refer :all]
            [authenticated-compojure-api.handler :refer :all]
            [authenticated-compojure-api.test-utils :as helper]
            [authenticated-compojure-api.queries.query-defs :as query]
            [ring.mock.request :as mock]
            [cheshire.core :as ch]))

(defn setup-teardown [f]
  (try
    (query/insert-permission<! {:permission "basic"})
    (query/insert-permission<! {:permission "admin"})
    (query/insert-permission<! {:permission "other"})
    (helper/add-users)
    (f)
    (finally (query/truncate-all-tables-in-database!))))

(use-fixtures :once helper/create-tables)
(use-fixtures :each setup-teardown)

(deftest can-add-user-permission-with-valid-token-and-admin-permissions
  (testing "Can add user permission with valid token and admin permissions"
    (let [user-id-1         (:id (first (query/get-registered-user-by-username {:username "JarrodCTaylor"})))
          user-id-2         (:id (first (query/get-registered-user-by-username {:username "Everyman"})))
          _                 (is (= "basic" (:permissions (first (query/get-permissions-for-userid {:userid user-id-1})))))
          _                 (query/insert-permission-for-user<! {:userid user-id-1 :permission "admin"})
          response          (app (-> (mock/request :post (str "/api/permission/user/" user-id-2) (ch/generate-string {:permission "other"}))
                                     (mock/content-type "application/json")
                                     (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body              (helper/parse-body (:body response))
          expected-response (str "Permission 'other' for user " user-id-2 " successfully added")]
      (is (= 200               (:status response)))
      (is (= expected-response (:message body)))
      (is (= "basic,other"     (helper/get-permissions-for-user user-id-2))))))

(deftest attempting-to-add-a-permission-that-does-not-exist-returns-404
  (testing "Attempting to add a permission that does not exist returns 404"
    (let [user-id-1         (:id (first (query/get-registered-user-by-username {:username "JarrodCTaylor"})))
          user-id-2         (:id (first (query/get-registered-user-by-username {:username "Everyman"})))
          _                 (query/insert-permission-for-user<! {:userid user-id-1 :permission "admin"})
          _                 (is (= "basic" (:permissions (first (query/get-permissions-for-userid {:userid user-id-2})))))
          response (app (-> (mock/request :post (str "/api/permission/user/" user-id-2) (ch/generate-string {:permission "stranger"}))
                            (mock/content-type "application/json")
                            (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 404                                    (:status response)))
      (is (= "Permission 'stranger' does not exist" (:error body)))
      (is (= "basic"                                (helper/get-permissions-for-user user-id-2))))))

(deftest can-not-add-user-permission-with-valid-token-and-no-admin-permissions
  (testing "Can not add user permission with valid token and no admin permissions"
    (let [user-id-1         (:id (first (query/get-registered-user-by-username {:username "Everyman"})))
          _                 (is (= "basic" (:permissions (first (query/get-permissions-for-userid {:userid user-id-1})))))
          response (app (-> (mock/request :post (str "/api/permission/user/" user-id-1)  (ch/generate-string {:permission "other"}))
                            (mock/content-type "application/json")
                            (helper/get-token-auth-header-for-user "Everyman:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 401              (:status response)))
      (is (= "Not authorized" (:error body)))
      (is (= "basic"          (helper/get-permissions-for-user user-id-1))))))
