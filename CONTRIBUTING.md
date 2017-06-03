# Contribution Guidelines

## Pull requests (PRs) are always welcome

We're trying very hard to keep our systems simple, lean and focused. We don't
want them to be everything for everybody. This means that we might decide
against incorporating a new request.


## Create issues ...

Please document any significant change as a GitHub issue before you
start working on it.


### ...but check for existing issues first!

Please check that an issue documenting your request doesn't already exist. If it does, it never hurts to add a quick "+1" or "I need this
too". This will help prioritize the most common requests.


## Conventions

Fork the repository and make changes on your fork on a branch:

1. Create the right type of issue (defect, enhancement, tast, etc.)
2. Name the branch N-something, where N is the issue number.

The maintainers work on branches in this repository.

Work hard to ensure your PR is valid.

Make PR descriptions as clear as possible and include a
reference to all the issues that they address. In GitHub, you can reference an
issue by adding a line to your commit description that follows the format:

    Fixes #N

where N is the issue number.


## Merge approval

Repository maintainers use LGTM (Looks Good To Me) in code-review comments to indicate acceptance.

A change requires LGTMs from an absolute majority of the MAINTAINERS. The
Benevolent Dictator for Life reserves sole veto power. We recommend also
getting an LGTM from the BDFL prior to merging, to avoid a potential
a revert.


#### Small patch exception

There are exceptions to the merge approval process. Currently these are:

* Your patch fixes spelling or grammar errors.
* Your patch fixes Markdown formatting or syntax errors in any .md files in
  this repository.


## How can I become a maintainer?

**Make important contributions**.

Don't forget: Being a maintainer is a time investment. Make sure you will have
time to be available. You don't have to be a maintainer to make a
difference on the project!


## What is a maintainer's responsibility?

It is every maintainer's responsibility to:

1. Deliver prompt feedback and decisions on PRs.
2. Be available to anyone with questions, bug reports, criticisms etc. on
   their component. This includes HipChat and GitHub requests.
3. Ensure their component respects the project's philosophy, design and
   roadmap.


## How are decisions made?

Short answer: with PRs to this repository.

All decisions, big and small, follow the same thre steps:

1. Open a PR. Anyone can do this.
2. Discuss the PR. Anyone can do this.
3. Accept (`LGTM`) or refuse a PR. The relevant maintainers
   do this. (See below, "Who decides what?")

   A. Accepting PRs
      1. If the PR appears to be ready to merge, give it a `LGTM`, which stands for "Looks Good To Me."
      2. If the PR has some small problems that require fixes, make a comment adressing the issues.
      3. If the required changes to a PR are small, you can add a "LGTM once the following comments are adressed..." This will reduce needless back-and-forth.
      4. If the PR only needs a few changes before being merged, any MAINTAINER can make a replacement PR that incorporates the existing commits and fixes the problems before a fast-track merge.

   B. Closing PRs
      1. If a PR appears to be abandoned, first try to contact the original contributor. If you don't get a response, then make a replacement PR. After that, any contributor may close the original PR.
      2. If you're unsure whether the PR implements a good feature, or if you don't understand the PR's purpose, ask the contributor to provide more documentation. If the contributor is not able to adequately explain the PR's purpose, any MAINTAINER may close the PR.
      3. If a MAINTAINER believes the PR includes significant architectural flaws, or if the PR requires significantly more design discussion before consideration, the MAINTAINER should close the PR with a short explanation. It is important not to leave such PRs open, as this will waste both the MAINTAINER's time and the contributor's time. It's not good to string along a contributor for weeks or months, asking them to make many changes to a PR that we'll eventually reject.


## Who decides what?

All decisions are PRs, and the relevant maintainers make decisions
by either merging or rejecting them. Review and acceptance by anyone is
denoted by adding a comment in the PR: `LGTM`. However, only
currently listed `MAINTAINERS` count toward the required majority.

Event repositories follow the timeless, highly efficient and totally unfair
system known as [Benevolent Dictator for
Life](http://en.wikipedia.org/wiki/Benevolent_Dictator_for_Life). This means
that the BDFL makes all decisions, in the end, by default. In
practice, we spread all decisions across the maintainers with the goal of achieving consensus
prior to all merges.

The current BDFL is listed by convention in the first line of the MAINTAINERS
file with a suffix of "BDFL".


## I'm a maintainer, should I make PRs too?

Yes. Nobody should ever push to master directly. All changes should be made
through a PR.


## Who assigns maintainers?

MAINTAINERS change via PRs and the standard approval
process - i.e. create an issue and make a PR containing the
changes to the MAINTAINERS file.


## How does this process change?

Just like everything else: by making a PR. :)
