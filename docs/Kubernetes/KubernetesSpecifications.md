
## K8s API endpoints:

### Kubernetes API server:

```shell
kubectl proxy -p 8080
```

Then open `http://localhost:8080` in browser.

For OpenAPI spec open `http://localhost:8080/openapi/v2`.

## Names

Depending on the requirements RFC 1123, RFC 1123 Label Names or RFC 1035 apply:
- contain no more than 253 / 63 characters
- contain only lowercase alphanumeric characters, '-' or '.' / contain only lowercase alphanumeric characters or '-'
- start with an alphanumeric / alphabetic character
- end with an alphanumeric / alphabetic character

See https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names


## Namespace names

When you create a Service, it creates a corresponding DNS entry. This entry is of the form `<service-name>.<namespace-name>.svc.cluster.local`.
As a result, all namespace names must be valid RFC 1123 DNS labels (see above [Names](#names)):

- contain at most 63 characters
- contain only lowercase alphanumeric characters or '-'
- start with an alphanumeric character
- end with an alphanumeric character


## Initial namespaces

Kubernetes starts with four initial namespaces:

### default
Kubernetes includes this namespace so that you can start using your new cluster without first creating a namespace.

### kube-node-lease
This namespace holds Lease objects associated with each node. Node leases allow the kubelet to send heartbeats so that the control plane can detect node failure.

### kube-public
This namespace is readable by all clients (including those not authenticated). This namespace is mostly reserved for cluster usage, in case that some resources should be visible and readable publicly throughout the whole cluster. The public aspect of this namespace is only a convention, not a requirement.

### kube-system
The namespace for objects created by the Kubernetes system.

See https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces/#initial-namespaces

