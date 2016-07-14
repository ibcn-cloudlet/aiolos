The implementations of the Resource API used by the
repository are hosted here in an exported package.
This allows to serialize Resource/Capability/Requirement
objects over the network as arguments of Remote Services,
allowing to host only one Repository instance on one machine
in the network.

This has some severe implications on the current implementation
and maybe we should re-think this in the future ...

Cons:
- public addXxx methods in objects that suppose to be immutable
- the Repository becomes more AIOLOS specific and is not general purpose spec implementation
- dependency on resource impls in both DeploymentManager and Repository,
while the OSGi Resource interfaces should suffice to have correct types...

