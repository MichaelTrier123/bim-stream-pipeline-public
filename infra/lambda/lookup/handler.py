import json
import os
import boto3
import pg8000.native

secrets = boto3.client("secretsmanager")

def resp(status, body):
    return {
        "statusCode": status,
        "headers": {"content-type": "application/json"},
        "body": json.dumps(body, default=str),
    }

def get_db_config():
    secret_arn = os.environ["DB_SECRET_ARN"]
    val = secrets.get_secret_value(SecretId=secret_arn)
    s = json.loads(val["SecretString"])
    return {
        "host": s["host"],
        "port": int(s.get("port", 5432)),
        "user": s["username"],
        "password": s["password"],
        "database": s.get("dbname") or s.get("database") or "bim_stream",
    }

def main(event, context):
    params = event.get("queryStringParameters") or {}
    key = params.get("key")
    if not key:
        return resp(400, {"message": "Missing required query param: key"})

    cfg = get_db_config()

    try:
        conn = pg8000.native.Connection(
            user=cfg["user"],
            password=cfg["password"],
            host=cfg["host"],
            port=cfg["port"],
            database=cfg["database"],
            timeout=5,
        )
        rows = conn.run("""
            SELECT
              id,
              created_at,
              source_bucket,
              source_key,
              ifc_schema,
              source_application,
              project_name,
              site_name,
              building_name
            FROM model_import
            WHERE source_key = :key
            LIMIT 1
        """, key=key)

        if not rows:
            conn.close()
            return resp(404, {"message": "Import not found", "key": key})

        (
            import_id,
            created_at,
            source_bucket,
            source_key,
            ifc_schema,
            source_application,
            project_name,
            site_name,
            building_name,
        ) = rows[0]

        counts = conn.run("""
            SELECT element_type, count
            FROM model_element_count
            WHERE model_import_id = :id
            ORDER BY element_type
        """, id=import_id)

        conn.close()

        element_counts = [
            {"elementType": et, "count": c}
            for (et, c) in counts
        ]

        return resp(200, {
            "key": key,
            "import": {
                "id": import_id,
                "createdAt": created_at,
                "sourceBucket": source_bucket,
                "sourceKey": source_key,
                "ifcSchema": ifc_schema,
                "sourceApplication": source_application,
                "projectName": project_name,
                "siteName": site_name,
                "buildingName": building_name,
                "elementCounts": element_counts
            }
        })

    except Exception as e:
        return resp(500, {
            "message": "Lookup failed",
            "error": str(e)
        })
