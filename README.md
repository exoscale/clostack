clostack: clojure cloudstack client
===================================

[![Build Status](https://secure.travis-ci.org/pyr/clostack.png)](http://travis-ci.org/pyr/clostack)


A simple clojure cloudstack client.

## Reasoning

This client provides two ways to query the cloudstack
HTTP API.

* A standard synchronous way, which still pools outgoing requests.
* A lower level access to the internal HTTP asynchronous client.

## Configuring

Client are created with the `http-client` which takes an optional
argument map:

* `config`: a map of three keys:
  * `api-key`: cloustack api key
  * `api-secret`: cloustack api secret
  * `endpoint`: URL where cloudstack lives
* `client`: a netty HTTP client, as provided by https://github.com/pyr/net  

The `api-key`, `api-secret` and `endpoint` may also be fetched from the environment
or a configuration file.

The environment takes precedence over the configuration file. When looking for environment
variables, both actual environment variables and JVM system properties will be looked up.


The following system properties or environment variables can be
used to provide settings:

<table>
<tr><th>Property</th><th>Environment</th><th>Description</th></tr>
<tr><td>clostack.api.key</td><td>CLOSTACK_API_KEY</td><td>Cloudstack API key</td></tr>
<tr><td>clostack.api.secret</td><td>CLOSTACK_API_SECRET</td><td>Cloudstack API secret</td></tr>
<tr><td>clostack.endpoint</td><td>CLOSTACK_ENDPOINT</td><td>Cloudstack API endpoint</td></tr>
</table>

The configuration file allows for different profiles, two additional relevant variables may be set:

<table>
<tr><th>Property</th><th>Environment</th><th>Description</th></tr>
<tr><td>clostack.profile</td><td>CLOSTACK_PROFILE</td><td>Configuration profile name</td></tr>
<tr><td>clostack.config.file</td><td>CLOSTACK_CONFIG_FILE</td><td>Configuration file location</td></tr>
</table>

A typical configuration will have the following aspect:

```clojure
{:default :prod
 :prod    {:api-key    "xxxx"
           :api-secret "xxxx"
           :endpoint   "https://..."}
 :staging {:api-key    "xxxx"
           :api-secret "xxxx"
           :endpoint   "https://..."}}
```

In this configuration, we define two cloudstack installations, by default the production credentials will
be used, setting clostack.profile to `staging` would switch this mode of operation.

## Using in your project

The easiest way to use clostack in your own projects is via Leiningen. Add the following dependency to your project.clj file:

```clojure
[spootnik/clostack "0.2.9"]
```

## Building Documentation

run `lein doc` 

## Sample Code

```clojure
(def client (http-client {}))

;; Simple requests
(request client :list-virtual-machines {})
(request client :list-zones {})

;; Provide a callback to execute when response comes back
(async-request client :list-service-offerings {} (fn [resp] ...))

;; Page through all records and return a consistent lazy map
(paging-request client :list-events {})

;; Execute body when response comes back
(with-response [resp client :list-events]
  (println (pr-str resp)))
```
