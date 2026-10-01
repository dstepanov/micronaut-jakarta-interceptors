# Parameter-array interoperability investigation

Jakarta Interceptors 2.2 section 2.4 and the `InvocationContext` API specify the values returned by
`getParameters`, replacement through `setParameters`, and rejection of incorrect argument counts or types.
They do not specify identity or live aliasing for either array.

Sources:
- https://jakarta.ee/specifications/interceptors/2.2/jakarta-interceptors-spec-2.2.pdf
- https://jakarta.ee/specifications/interceptors/2.2/apidocs/jakarta.interceptor/jakarta/interceptor/invocationcontext

Weld 5.1.7.Final and 6.0.4.Final expose live parameter-array mutations in the differential corpus. Micronaut
copies getter results and copies validated setter values. The two differing rows therefore describe an array
ownership policy, rather than an established specification violation. Portable interceptors should explicitly
call `setParameters` after editing their argument values.

The published CDI TCK 5.0.0-M1 interceptor sources compare the values after setters and test invalid setters.
Their invocation-context and around-construction scenarios do not assert array identity or subsequent array
mutation. This is evidence about the inspected test version, not a guarantee about every TCK release.

`ParameterArrayOwnershipTest` exercises both managed method and constructor interception. It checks getter
and setter copies, successful replacement reaching the target, and atomic rejection of incorrect type/count
and null arrays. Its ownership assertions are labeled as Micronaut policy, not normative TCK assertions.

The runtime comment formerly claimed the specification prohibited getter-array mutation reaching the
invocation. This claim was too strong; the updated comment distinguishes the contract from implementation
policy. `ParameterSnapshotTest` deterministically changes the caller's array while the invocation metadata is
being read, and verifies that the original valid snapshot is installed. Setter values are now copied before validation so that the values validated are exactly the values
installed. This prevents caller mutation between validation and copying from introducing an unchecked value.
No runtime aliasing policy changes.
