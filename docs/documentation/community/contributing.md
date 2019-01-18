---
layout: documentation
---

{% include base.html %}

# How To Become Part of the Community

## Getting Involved

There are several ways of how to get in contact with the community in order to ask questions or discuss ideas:

## Discussion Forum

The forum is used to discuss ideas and to answer general questions. It is organized per topic, so you can easily decide what is of interest for you.

* [Discussion Forum](http://eclipse.org/forums/eclipse.smarthome)

## Issue Tracker

Like most GitHub hosted projects, we use GitHub Issues as an issue tracking system.

* [Eclipse SmartHome Issues](https://github.com/eclipse/smarthome/issues)

If you have found a bug or if you would like to propose a new feature, please feel free to enter an issue. But before creating a new issue, please first check that it does not already exist.

## Report a Security Vulnerability

Bugs which are crucial for the security of the project should be reported as a security vulnerability to the Eclipse Security Team:

* [Eclipse Security Team](https://eclipse.org/security)

### Issue Tracker Links

* [Create a new Issue](https://github.com/eclipse/smarthome/issues/new)
* [All Issues](https://github.com/eclipse/smarthome/issues?utf8=%E2%9C%93&q=is%3Aissue)
* [Open Bugs](https://github.com/eclipse/smarthome/issues?q=is%3Aopen+is%3Aissue+label%3Abug)
* [Open Features](https://github.com/eclipse/smarthome/issues?utf8=%E2%9C%93&q=is%3Aopen+is%3Aissue+label%3Aenhancement+)
* [New/Unconfirmed Issues](https://github.com/eclipse/smarthome/issues?q=is%3Aopen+is%3Aissue+no%3Aassignee)
 
# Code Contributions

If you want to become a contributor to the project, please check our guidelines first. If you can't wait to get your hands dirty have a look at the open issues where we [need your help](https://github.com/eclipse/smarthome/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22) or one of the [open bugs](https://github.com/eclipse/smarthome/issues?q=is%3Aissue+is%3Aopen+label%3Abug).
Be aware that you are required to sign the [Eclipse Contributor Agreement](https://www.eclipse.org/legal/ECA.php) in order for us to accept your contribution.

## Pull requests are always welcome

We are always thrilled to receive pull requests, and do our best to process them as fast as possible. Not sure if that typo is worth a pull request? Do it! We will appreciate it.

If your pull request is not accepted on the first try, don't be discouraged! If there's a problem with the implementation, hopefully you received feedback on what to improve.

We're trying very hard to keep Eclipse SmartHome lean and focused. We don't want it to do everything for everybody. This means that we might decide against incorporating a new feature. However, there might be a way to implement that feature on top of Eclipse SmartHome.

## Discuss your design in the discussion forum

We recommend discussing your plans in the [Discussion Forum](https://www.eclipse.org/forums/eclipse.smarthome) before starting to code - especially for more ambitious contributions. This gives other contributors a chance to point you in the right direction, give feedback on your design, and maybe point out if someone else is working on the same thing.

## Conventions for pull requests

* Submit unit tests for your changes. Eclipse SmartHome has a great test framework built in - use it! Take a look at existing tests for inspiration. Run the full test suite on your branch before submitting a pull request.
* Update the documentation when creating or modifying features. Test your documentation changes for clarity, concision and correctness, as well as a clean documentation build.
* Write clean code. Universally formatted code promotes ease of writing, reading and maintenance. Check our [Coding Guidelines](../development/guidelines.html).
* Pull requests' descriptions should be as clear as possible and include a reference to all the issues that they address.
* Pull requests must not contain commits from other users or branches.

The process to create a pull request is then the following:

1. Create an account at Eclipse if you do not have one yet.
1. Sign the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php).
For legal reasons you are required to sign the ECA before we can accept your pull request.
1. Fork the sources of Eclipse SmartHome on GitHub.
1. Do the coding!
1. Make sure your code applies to the [Coding Guidelines](../development/guidelines.html).
1. Make sure there is a [GitHub issue](https://github.com/eclipse/smarthome/issues?utf8=%E2%9C%93&q=is%3Aissue) for your contribution. If it does not exist yet, [create one](https://github.com/eclipse/smarthome/issues/new).
1. Add a "Signed-off-by" line to every commit you do - see the [Eclipse wiki here](https://wiki.eclipse.org/Development_Resources/Contributing_via_Git#Signing_off_on_a_commit) and [here](https://wiki.eclipse.org/Development_Resources/Contributing_via_Git#via_GitHub) for details.
1. Create a pull request, referencing the GitHub issue number.
