#!/bin/bash

# Set variables
USERNAME=${MAVEN_USERNAME:-"example_username"}
PASSWORD=${MAVEN_PASSWORD:-"example_password"}
BUNDLE_FILE="./build/distributions/openssl-3.4.0-beta-1.zip"

# Generate Bearer token
TOKEN=$(printf "$USERNAME:$PASSWORD" | base64)

# Upload the deployment bundle
echo "Uploading the deployment bundle..."
RESPONSE=$(curl --silent --request POST \
  --header "Authorization: Bearer $TOKEN" \
  --form "bundle=@$BUNDLE_FILE" \
  'https://central.sonatype.com/api/v1/publisher/upload')

DEPLOYMENT_ID=$(echo "$RESPONSE" | tr -d '\n')

if [ -z "$DEPLOYMENT_ID" ]; then
  echo "Failed to upload the bundle. Exiting."
  exit 1
fi

echo "Deployment ID: $DEPLOYMENT_ID"

# Poll the status until it's validated or fails
STATUS="PENDING"
while [[ "$STATUS" == "PENDING" || "$STATUS" == "VALIDATING" ]]; do
  echo "Checking deployment status..."
  sleep 10

  RESPONSE=$(curl --silent --request POST \
    --header "Authorization: Bearer $TOKEN" \
    "https://central.sonatype.com/api/v1/publisher/status?id=$DEPLOYMENT_ID")

  STATUS=$(echo "$RESPONSE" | jq -r '.deploymentState')

  echo "Current Status: $STATUS"

  if [[ "$STATUS" == "FAILED" ]]; then
    echo "Deployment failed. Exiting."
    exit 1
  fi
done

# If the deployment is validated, proceed to publish
if [[ "$STATUS" == "VALIDATED" ]]; then
  echo "Publishing the deployment..."
  PUBLISH_RESPONSE=$(curl --silent --request POST \
    --header "Authorization: Bearer $TOKEN" \
    "https://central.sonatype.com/api/v1/publisher/deployment/$DEPLOYMENT_ID")

  if [ $? -eq 0 ]; then
    echo "Deployment is now being published.
  else
    echo "Failed to publish the deployment. Exiting."
    exit 1
  fi
else
  echo "Deployment status is not validated. Status: $STATUS"
fi
