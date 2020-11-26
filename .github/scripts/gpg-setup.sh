# Don't edit this file!
# It is automatically updated after every release of https://github.com/alejandrohdezma/sbt-ci
# If you want to suggest a change, please open a PR or issue in that repository

#!/usr/bin/env sh

echo $PGP_SECRET | base64 --decode | gpg --import --no-tty --batch --yes

echo "allow-loopback-pinentry" >>~/.gnupg/gpg-agent.conf
echo "pinentry-mode loopback" >>~/.gnupg/gpg.conf

gpg-connect-agent reloadagent /bye