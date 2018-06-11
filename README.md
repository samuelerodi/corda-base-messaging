![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# CorDapp Base Messaging

This CordApp is a template which implements a basic messaging protocol on top of the Corda architecture.
With this CordApp it is possible to send a signed message to a peer that also needs to sign in order to prove the message delivery. In future versions the recipient could also eventually use and spend the message as proof to be the legitimate owner or user of the message.
It is also possible to choose a handful of recipients for a message or broadcast it to the entire network. Also in this case it will be possible to specify a single user/owner of the message with a rule first-come-first-served.

This is an example repository with the purpose of demonstrating of the Corda functionalities and peer-to-peer architecture. The feature implemented in this repository might be a starting point for a set of diverse use cases where value-messaging in all of its form is handled over a network and double-spending represent an issue.

Feel free to contribute.

For further documentation on the basic usage of Corda check [here](http://docs.corda.net/tutorial-cordapp.html).
