from openai import OpenAI
import anthropic

class Research:
    def __init__(self, oapi_key, anth_api_key):
        self.openai_api_key = oapi_key
        self.anthropic_api_key = anth_api_key

    def query_gpt(self, prompt):
        try:
            client = OpenAI(api_key=self.openai_api_key)
            response = client.chat.completions.create(model="gpt-4",
            messages=[
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": prompt}
            ])
            return response.choices[0].message.content.strip()
        except Exception as e:
            return f"An error occurred: {str(e)}"

    def query_claude(self, prompt):
        try:
            client = anthropic.Anthropic(
                api_key=self.anthropic_api_key,
            )
            message = client.messages.create(
                model="claude-3-5-sonnet-20240620",
                max_tokens=1024,
                messages=[
                    {"role": "user", "content": prompt}
                ]
            )
            return message.count

        except Exception as e:
            return f"An error occurred: {str(e)}"



    def combo_query(self, prompt):
        gpt_response = self.query_gpt(prompt)
        claude_response = self.query_claude(prompt)
        return gpt_response, claude_response
