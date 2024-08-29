from .models import EmailConfig

class AppConfigs:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # Ensure that an EmailConfig entry exists
        default_config = {
            "from_email_name": 'BipolarBuddy Team',
            "from_email": 'bengomallKE@gmail.com',
            "email_password": 'obqvwmnpioaclyhj',
            "email_host": 'smtp.gmail.com',
            "email_port": '587',
            "use_tls": True,
            "fail_silently": False,
        }
        EmailConfig.objects.update_or_create(
            id=1,  # Assuming you want to ensure a single configuration entry
            defaults=default_config
        )

        response = self.get_response(request)
        return response
