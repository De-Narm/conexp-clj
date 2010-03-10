#!/bin/bash

mkdir -p lib && cp src/lib/*.jar lib/ && cp src/lib/*.clj lib && \
lein deps && lein jar && mv conexp.jar lib && \
zip conexp-clj.zip bin lib AUTHORS LICENSE README
