import importlib.util
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("313_team_server.py")
SPEC = importlib.util.spec_from_file_location("team_gateway", MODULE)
gateway = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(gateway)


class GatewayPolicyTests(unittest.TestCase):
    def test_loopback_binding_only(self):
        self.assertEqual(gateway.HOST, "127.0.0.1")

    def test_local_omniroute_is_the_only_configured_inference_gateway(self):
        self.assertEqual(gateway.OMNIROUTE_DEFAULT_URL, "http://127.0.0.1:20128")
        source = MODULE.read_text(encoding="utf-8")
        self.assertIn('config["omniroute_model"]', source)
        self.assertNotIn("openrouter.ai/api/v1/chat/completions", source)
        self.assertNotIn("api.runcrate.ai", source)

    def test_strict_free_gate_fails_closed(self):
        source = MODULE.read_text(encoding="utf-8")
        self.assertIn("def require_strict_free(config):", source)
        self.assertIn('config.get("omniroute_strict_free_confirmed") is not True', source)
        self.assertIn("require_strict_free(config)", source)

    def test_no_paid_fallback(self):
        source = MODULE.read_text(encoding="utf-8")
        self.assertIn('"paid_fallback": False', source)
        self.assertIn("if cost_value is not None and cost_value > 0:", source)

    def test_diagnostics_do_not_claim_completion_was_tested(self):
        source = MODULE.read_text(encoding="utf-8")
        self.assertIn('result["model_completion_tested"] = False', source)
        self.assertIn('result["omniroute_ok"]', source)

    def test_secret_paths_are_excluded(self):
        self.assertIsNotNone(gateway.SECRET_NAME.search(".env"))
        self.assertIsNotNone(gateway.SECRET_NAME.search("app/private_key.pem"))

    def test_source_secrets_are_redacted(self):
        sample = "token=ghp_" + "A" * 30 + " api=sk-or-v1-" + "B" * 30
        cleaned = gateway.redact_secrets(sample)
        self.assertNotIn("ghp_", cleaned)
        self.assertNotIn("sk-or-v1-", cleaned)
        self.assertIn("[REDACTED_SECRET]", cleaned)

    def test_local_credential_files_are_excluded(self):
        self.assertIn("local.properties", gateway.SECRET_BASENAMES)
        self.assertIn("gradle.properties", gateway.SECRET_BASENAMES)

    def test_binary_and_build_directories_are_excluded(self):
        self.assertIn("build", gateway.SKIP_PARTS)
        self.assertIn(".gradle", gateway.SKIP_PARTS)


if __name__ == "__main__":
    unittest.main()
