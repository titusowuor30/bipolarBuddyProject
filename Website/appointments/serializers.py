from rest_framework import serializers
from .models import Appointment
from django.contrib.auth import get_user_model

User = get_user_model()

class AppointmentSerializer(serializers.ModelSerializer):
    user_email = serializers.EmailField(write_only=True)
    user_name = serializers.CharField(write_only=True)
    user_phone = serializers.CharField(write_only=True)

    class Meta:
        model = Appointment
        fields = ['user', 'user_email', 'user_name', 'user_phone', 'date', 'status', 'department', 'doctor', 'message']

    def create(self, validated_data):
        user_email = validated_data.pop('user_email')
        user_name = validated_data.pop('user_name')
        user_phone = validated_data.pop('user_phone')

        # Retrieve or create a user
        request_user = self.context['request'].user

        if request_user.is_authenticated:
            user = request_user
        else:
            # Check if user exists or create a new user
            user, created = User.objects.get_or_create(
                email=user_email,
                defaults={
                    'username': user_email,
                    'first_name': user_name.split(' ')[0] if ' ' in user_name else user_name,
                    'last_name': user_name.split(' ')[1] if ' ' in user_name else '',
                    'phone': user_phone
                }
            )

        # Create the appointment with the user
        appointment = Appointment.objects.create(user=user, **validated_data)
        return appointment
