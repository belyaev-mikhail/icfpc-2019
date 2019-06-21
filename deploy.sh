#!/bin/env bash

cd $1
zip solutions.zip *
curl -F "private_id=fb25f23384c48c7074b9930c" -F "file=@solutions.zip" https://monadic-lab.org/submit
