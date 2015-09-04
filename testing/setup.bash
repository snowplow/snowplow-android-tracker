#!/bin/bash
## Configures a test environment for the 
## android tracker.

## Start Mountebank and Ngrok and create an imposter
echo "Launching Mountebank and Ngrok"

ngrok http -config=/vagrant/testing/ngrok.yml -log=stdout 4545 > /dev/null &
mb > /dev/null &
sleep 5
curl -i -X POST -H 'Content-Type: application/json' -d@/vagrant/testing/imposter.json http://localhost:2525/imposters

echo "Mountebank and Ngrok are now running..."
