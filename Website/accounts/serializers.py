from rest_framework import serializers
from .models import *
class CustomUserSerializer(serializers.ModelSerializer):
    class Meta:
        model = CustomUser
        fields = ['username', 'email', 'password', 'first_name', 'last_name', 'gender', 'phone','height','weight','age']
        extra_kwargs = {
            'password': {'write_only': True},
        }

    def create(self, validated_data):
        user = CustomUser(
            email=validated_data['email'],
            username=validated_data['username'],
            first_name=validated_data['first_name'],
            last_name=validated_data['last_name'],
            gender=validated_data.get('gender'),
            phone=validated_data.get('phone'),
            age=validated_data.get('age'),
            height=validated_data.get('height'),
            weight=validated_data.get('weight')
        )
        user.set_password(validated_data['password'])
        user.save()
        return user


class PatientSerializer(serializers.ModelSerializer):
    user = CustomUserSerializer()

    class Meta:
        model = Patient
        fields = ['user', 'doctor']

    def create(self, validated_data):
        user_data = validated_data.pop('user')
        user_serializer = CustomUserSerializer(data=user_data)
        user_serializer.is_valid(raise_exception=True)
        user = user_serializer.save()

        patient = Patient.objects.create(user=user, **validated_data)
        return patient

class DoctorSerializer(serializers.ModelSerializer):
    name = serializers.SerializerMethodField()

    class Meta:
        model = Doctor
        fields = ['id', 'specialization', 'name']

    def get_name(self, obj):
        return f"{obj.user.first_name} {obj.user.last_name}"