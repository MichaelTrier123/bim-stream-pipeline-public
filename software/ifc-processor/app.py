from flask import Flask, request, jsonify
import os
import logging

from elasticapm.contrib.flask import ElasticAPM
import ifcopenshell

app = Flask(__name__)
apm_config = {
    'SERVICE_NAME': os.environ.get("ELASTIC_APM_SERVICE_NAME", "ifc-processor-python"),
    'ENVIRONMENT': os.environ.get("ELASTIC_APM_ENVIRONMENT", "dev"),
    'SECRET_TOKEN': os.environ.get("ELASTIC_APM_SECRET_TOKEN"),
    'SERVER_URL': os.environ.get("ELASTIC_APM_SERVER_URL"),
}

apm = ElasticAPM(app, **apm_config)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("IfcProcessor")
@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "Python sidecar is healthy"}), 200


@app.post("/process")
def process_ifc():
    data = request.get_json(force=True) or {}
    ifc_path = data.get("path")

    if not ifc_path:
        return jsonify({"status": "error", "message": "Missing 'path' parameter"}), 400

    if not os.path.exists(ifc_path):
        return jsonify({"status": "error", "message": f"File not found: {ifc_path}"}), 400

    try:
        model = ifcopenshell.open(ifc_path)

        # --- schema (IFC2X3 / IFC4)
        ifc_schema = None
        try:
            ifc_schema = model.schema
        except Exception:
            pass

        # --- source application (best-effort)
        source_app = None
        try:
            apps = model.by_type("IfcApplication")
            if apps:
                a = apps[0]
                parts = []
                if getattr(a, "ApplicationFullName", None):
                    parts.append(str(a.ApplicationFullName))
                elif getattr(a, "ApplicationIdentifier", None):
                    parts.append(str(a.ApplicationIdentifier))
                if getattr(a, "Version", None):
                    parts.append(str(a.Version))
                source_app = " ".join(parts) if parts else None
        except Exception:
            pass

        # --- project/site/building names (best-effort)
        def safe_name(obj):
            try:
                if obj and getattr(obj, "Name", None):
                    return str(obj.Name)
            except Exception:
                pass
            return None

        project_name = None
        site_name = None
        building_name = None

        try:
            projects = model.by_type("IfcProject")
            if projects:
                project_name = safe_name(projects[0])
        except Exception:
            pass

        try:
            sites = model.by_type("IfcSite")
            if sites:
                site_name = safe_name(sites[0])
        except Exception:
            pass

        try:
            buildings = model.by_type("IfcBuilding")
            if buildings:
                building_name = safe_name(buildings[0])
        except Exception:
            pass


        counts = {
            "WALL": len(model.by_type("IfcWall")),
            "WINDOW": len(model.by_type("IfcWindow")),
            "DOOR": len(model.by_type("IfcDoor")),
            "FLOOR": len(model.by_type("IfcSlab")),
            "SPACE": len(model.by_type("IfcSpace")),
        }

        resp = {
            "status": "success",
            "message": None,
            "ifcSchema": ifc_schema,
            "sourceApp": source_app,
            "projectName": project_name,
            "siteName": site_name,
            "buildingName": building_name,
            "counts": counts,
        }

        logger.info("Processed %s: %s", ifc_path, counts)
        return jsonify(resp), 200

    except Exception as e:
        logger.exception("Error processing %s", ifc_path)
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)