import os

import anthropic
from openai import OpenAI


class ProviderNotConfiguredError(RuntimeError):
    """Raised when an AI provider is requested but no API key is configured."""


class Research:
    def __init__(self, config):
        self.openai_api_key = (config.get("openai") or {}).get("api_key") or os.getenv(
            "OPENAI_API_KEY"
        )
        self.anthropic_api_key = (
            (config.get("anthropic") or {}).get("api_key")
            or os.getenv("ANTHROPIC_API_KEY")
        )
        self._openai_client = None
        self._anthropic_client = None

    def has_openai(self):
        return bool(self.openai_api_key)

    def has_anthropic(self):
        return bool(self.anthropic_api_key)

    def has_any_provider(self):
        return self.has_openai() or self.has_anthropic()

    def _get_openai_client(self):
        if not self.has_openai():
            raise ProviderNotConfiguredError("OpenAI API key not configured.")
        if not self._openai_client:
            self._openai_client = OpenAI(api_key=self.openai_api_key)
        return self._openai_client

    def _get_anthropic_client(self):
        if not self.has_anthropic():
            raise ProviderNotConfiguredError("Anthropic API key not configured.")
        if not self._anthropic_client:
            self._anthropic_client = anthropic.Anthropic(api_key=self.anthropic_api_key)
        return self._anthropic_client

    def query_gpt(self, prompt):
        client = self._get_openai_client()
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": prompt},
            ],
        )
        return response.choices[0].message.content.strip()

    def query_claude(self, prompt):
        client = self._get_anthropic_client()
        message = client.messages.create(
            model="claude-3-5-sonnet-20240620",
            max_tokens=1024,
            messages=[{"role": "user", "content": prompt}],
        )
        return message.content[0].text

    def estimate(self, prompt):
        """
        Return an estimate response using the first configured provider.

        Preference order: OpenAI, then Anthropic.
        """
        if self.has_openai():
            return self.query_gpt(prompt)
        if self.has_anthropic():
            return self.query_claude(prompt)
        raise ProviderNotConfiguredError("No AI providers are configured.")

    def combo_query(self, prompt):
        gpt_response = "OpenAI provider not configured."
        claude_response = "Anthropic provider not configured."

        if self.has_openai():
            try:
                gpt_response = self.query_gpt(prompt)
            except Exception as exc:  # pragma: no cover - UI helper
                gpt_response = f"OpenAI query failed: {exc}"

        if self.has_anthropic():
            try:
                claude_response = self.query_claude(prompt)
            except Exception as exc:  # pragma: no cover - UI helper
                claude_response = f"Anthropic query failed: {exc}"

        return gpt_response, claude_response
