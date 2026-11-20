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

    def test_openrouter_free_router_is_the_only_default_target(self):
        self.assertEqual(gateway.OPENROUTER_URL, "https://openrouter.ai/api/v1/chat/completions")
        source = MODULE.read_text(encoding="utf-8")
        self.assertIn('"model": "openrouter/free"', source)
        self.assertNotIn('"models": [', source)

    def test_no_paid_fallback_flag_in_saved_status(self):
        source = MODULE.read_text(encoding="utf-8")
        self.assertIn('"paid_fallback": False', source)

    def test_main_chat_uses_only_free_router(self):
        source = MODULE.read_text(encoding="utf-8")
        self.assertIn('if self.path not in ("/api/team/review", "/api/chat")', source)
        self.assertIn('"model": "openrouter/free"', source)
        self.assertIn('def chat_completion(config, messages):', source)

    def test_diagnostics_validate_both_providers(self):
        source = MODULE.read_text(encoding="utf-8")
        self.assertIn('OPENROUTER_KEY_URL = "https://openrouter.ai/api/v1/key"', source)
        self.assertIn('def provider_diagnostics(config):', source)
        self.assertIn('"/api/diagnostics"', source)
        self.assertIn('"github_ok"', source)
        self.assertIn('"openrouter_ok"', source)

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
