from django.core.mail.backends.smtp import EmailBackend
from django.core.mail import EmailMessage
from django.utils.html import strip_tags
from datetime import datetime, timedelta
import re
from django.contrib.sites.models import Site
from datetime import datetime
from notifications.models import EmailConfig
import threading

class BipolarBuddyEmailBackend:
    def __init__(self, request, subject='Testing mails', body="Hi, there is a system generated test mail. Ignore if you are reading this!", to=["titusowuor30@gmail.com","wambui-thiongo@gmail.com"], attachments=[]):
        self.request = request
        self.subject = subject
        self.body = body
        self.to = to
        self.attachments = attachments

    def send_email(self):
        try:
            domain = self.request.META['HTTP_HOST']
            protocol = 'https' if self.request.is_secure() else 'http'
            site_login_url = str(protocol + '://' + str(domain)) + "/login"
            config = EmailConfig.objects.first()
            print(config)

            backend = EmailBackend(
                host=config.email_host,
                port=config.email_port,
                username=config.from_email,
                password=config.email_password,
                use_tls=config.use_tls,
                fail_silently=config.fail_silently
            )

            message = self.body + f"<br/><br/>Copyright @<a href='{site_login_url}'>BipolarBuddy. All Rights Reserved. {datetime.now().year}</a>"

            email = EmailMessage(
                subject=self.subject,
                body=message,
                from_email=config.from_email,
                to=self.to,
                connection=backend
            )
            email.content_subtype = "html"

            if len(self.attachments) > 0:
                print('check attachments...')
                for attch in self.attachments:
                    email.attach(attch.name, attch.read(), attch.content_type)

            email.send()
            print('Email sent successfully!')

        except Exception as e:
            print(e)
            print("Email send error:{}".format(e))

    def send_email_in_thread(self):
        email_thread = threading.Thread(target=self.send_email)
        email_thread.start()