#!/bin/env -S bash -u

cd $1
rm solutions.zip
zip solutions.zip *
curl -F "private_id=fb25f23384c48c7074b9930c" -F "file=@solutions.zip" https://monadic-lab.org/submit
