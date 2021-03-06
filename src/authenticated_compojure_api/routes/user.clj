(ns authenticated-compojure-api.routes.user
  (:require [authenticated-compojure-api.middleware.cors :refer [cors-mw]]
            [authenticated-compojure-api.middleware.authenticated :refer [authenticated-mw]]
            [authenticated-compojure-api.route-functions.user.create-user :refer [create-user-response]]
            [authenticated-compojure-api.route-functions.user.delete-user :refer [delete-user-response]]
            [authenticated-compojure-api.route-functions.user.modify-user :refer [modify-user-response]]
            [authenticated-compojure-api.auth-resources.token-auth-backend :refer [token-backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [schema.core :as s]
            [compojure.api.sweet :refer :all]))


(def user-routes
  "Specify routes for User functions"
  (context "/api/user" []

    (POST "/"           {:as request}
           :tags        ["User"]
           :return      {:username String}
           :middleware  [cors-mw]
           :body-params [email :- String username :- String password :- String]
           :summary     "Create a new user with provided username, email and password."
           (create-user-response email username password))

    (wrap-authentication
      (DELETE "/:id"       {:as request}
              :tags        ["User"]
              :path-params [id :- s/Uuid]
              :return      {:message String}
              :middleware  [cors-mw authenticated-mw]
              :summary     "Deletes the specified user. Requires token to have `admin` auth or self ID."
              :description "Authorization header expects the following format 'Token {token}'"
              (delete-user-response request id))
      token-backend)

    (wrap-authentication
      (PATCH  "/:id"         {:as request}
              :tags          ["User"]
              :path-params   [id :- s/Uuid]
              :body-params   [{username :- String ""} {password :- String ""} {email :- String ""}]
              :header-params [authorization :- String]
              :return        {:id s/Uuid :email String :username String}
              :middleware    [cors-mw authenticated-mw]
              :summary       "Update some or all fields of a specified user. Requires token to have `admin` auth or self ID."
              :description   "Authorization header expects the following format 'Token {token}'"
              (modify-user-response request id username password email))
      token-backend)))
