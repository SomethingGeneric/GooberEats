import openai
import anthropic

class Research:
    def __init__(self, api_key, anth_api_key):
        openai.api_key = api_key
        self.anthropic_api_key = anth_api_key

    def query_gpt(self, prompt):
        try:
            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are a helpful assistant."},
                    {"role": "user", "content": prompt}
                ]
            )
            return response.choices[0].message['content'].strip()
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
                    {"role": "system", "content": "You are a helpful assistant."},
                    {"role": "user", "content": prompt}
                ]
            )
            return message.count

        except Exception as e:
            return f"An error occurred: {str(e)}"
