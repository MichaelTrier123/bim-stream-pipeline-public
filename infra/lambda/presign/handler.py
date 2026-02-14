import json
import os
import uuid
import boto3
from datetime import datetime, timezone

s3 = boto3.client("s3")

BUCKET = os.environ["BUCKET_NAME"]
PREFIX = os.environ.get("UPLOAD_PREFIX", "uploads/")
EXPIRES = int(os.environ.get("URL_EXPIRES_SECONDS", "60"))

def _resp(status, body):
    return {
        "statusCode": status,
        "headers": {"content-type": "application/json"},
        "body": json.dumps(body),
    }

def main(event, context):
    key = f"{PREFIX}{uuid.uuid4()}.ifc"

    upload_url = s3.generate_presigned_url(
        ClientMethod="put_object",
        Params={
            "Bucket": BUCKET,
            "Key": key,
            "ContentType": "application/octet-stream",
        },
        ExpiresIn=EXPIRES,
    )

    return _resp(200, {
        "uploadUrl": upload_url,
        "key": key,
        "expiresSeconds": EXPIRES,
        "issuedAt": datetime.now(timezone.utc).isoformat()
    })
