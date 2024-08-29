from django.db import models
from django.contrib.auth import get_user_model
from django.core.validators import MinLengthValidator
User= get_user_model()

# Create your models here.
class Notification(models.Model):
    from_user = models.ForeignKey(User, on_delete=models.CASCADE,related_name='from_user',blank=True,null=True)
    to_user = models.ForeignKey(User, on_delete=models.CASCADE,related_name="to_user",blank=True,null=True)
    title = models.CharField(max_length=200)
    #title = models.PositiveBigIntegerField()
    message = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.title

class Reminders(models.Model):
    pass

class EmailConfig(models.Model):
    from_email_name = models.CharField(
        max_length=255, default='Bengomall Team', blank=True, null=True)
    from_email = models.EmailField(max_length=100,default="bengomallKE@gmail.com")
    email_password = models.CharField(
        max_length=128, validators=[MinLengthValidator(8)],default="obqvwmnpioaclyhj")
    email_host = models.CharField(max_length=50, default="smtp.gmail.com")
    email_port = models.CharField(max_length=5, default=587)
    use_tls = models.BooleanField(default=True)
    fail_silently = models.BooleanField(default=True)

    # def save(self, *args, **kwargs):
    #     self.email_password = make_password(self.email_password)
    #     super(EmailConfig, self).save(*args, **kwargs)

    def __str__(self) -> str:
        return self.from_email

    class Meta:
        verbose_name_plural = 'Email Configuration'