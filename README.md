clostack: clojure cloudstack client
===================================

A simple clojure cloudstack client.

## Reasoning

This client provides two ways to query the cloudstack
HTTP API.

* A standard synchronous way, which still pools outgoing requests.
* A lower level access to the internal HTTP asynchronous client.

## Configuring

Client are created with the `http-client` which takes the following
optional keyword arguments:

* `api-key`: cloustack api key
* `api-secret`: cloustack api secret
* `endpoint`: URL where cloudstack lives
* `http`: optional HTTP client

The following system properties or environment variables can be
used to provide settings:

<table>
<tr><th>Property</th><th>Environment</th><th>Description</th></tr>
<tr><td>cloudstack.api.key</td><td>CLOUDSTACK\_API\_KEY</td><td>Cloudstack API key</td></tr>
<tr><td>cloudstack.api.secret</td><td>CLOUDSTACK\_API\_SECRET</td><td>Cloudstack API secret</td></tr>
<tr><td>cloudstack.endpoint</td><td>CLOUDSTACK_ENDPOINT</td><td>Cloudstack API endpoint</td></tr>
</table>

## Using in your project

The easiest way to use clostack in your own projects is via Leiningen. Add the following dependency to your project.clj file:

```clojure
[spootnik/clostack "0.1.4"]
```

## Building Documentation

run `lein doc` 

## Sample Code

```clojure
(def client (http-client))
						 
(list-virtual-machines client)

(let [resp (async-list-virtual-machines client)]
  (http/await resp)
  (http/string resp))
```
